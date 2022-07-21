package org.sagebionetworks.bridge.json;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sagebionetworks.bridge.models.studies.Demographic;
import org.sagebionetworks.bridge.models.studies.DemographicId;
import org.sagebionetworks.bridge.models.studies.DemographicUser;
import org.sagebionetworks.bridge.models.studies.DemographicValue;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

// deserializes from assessment format
public class DemographicUserDeserializer extends JsonDeserializer<DemographicUser> {
    private final String MULTIPLE_SELECT_STEP_TYPE = "array";

    @Override
    public DemographicUser deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        DemographicAssessmentResults results = p.readValueAs(DemographicAssessmentResults.class);
        Map<String, Demographic> demographics = new ConcurrentHashMap<>();
        DemographicUser demographicUser = new DemographicUser(null, null, null, null, demographics);
        if (null != results.getStepHistory()) {
            for (DemographicAssessmentResultStep resultStep : results.getStepHistory()) {
                if (null == resultStep) {
                    continue;
                }
                if (null != resultStep.getValue()) {
                    // replace null with DemographicValue(null)
                    for (ListIterator<DemographicValue> iter = resultStep.getValue().listIterator(); iter.hasNext();) {
                        if (null == iter.next()) {
                            iter.set(new DemographicValue(null));
                        }
                    }
                }
                demographics.put(resultStep.getIdentifier(),
                        new Demographic(new DemographicId(null, resultStep.getIdentifier()), demographicUser,
                                null != resultStep.getAnswerType()
                                        && resultStep.getAnswerType().equalsIgnoreCase(MULTIPLE_SELECT_STEP_TYPE),
                                resultStep.getValue(), null));
            }
        }
        return demographicUser;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DemographicAssessmentResults {
        private List<DemographicAssessmentResultStep> stepHistory;

        public List<DemographicAssessmentResultStep> getStepHistory() {
            return stepHistory;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DemographicAssessmentResultStep {
        private String identifier;
        private String answerType;
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<DemographicValue> value;

        public String getIdentifier() {
            return identifier;
        }

        public String getAnswerType() {
            return answerType;
        }

        @JsonProperty("answerType")
        public void setAnswerType(Map<String, String> answerType) {
            this.answerType = answerType.get("type");
        }

        public List<DemographicValue> getValue() {
            return value;
        }
    }
}
