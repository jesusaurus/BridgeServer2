package org.sagebionetworks.bridge.models.upload;

import java.util.List;

import javax.annotation.Nonnull;

import org.sagebionetworks.bridge.exceptions.InvalidEntityException;
import org.sagebionetworks.bridge.models.BridgeEntity;
import org.sagebionetworks.bridge.models.healthdata.HealthDataRecord;
import org.sagebionetworks.bridge.validators.UploadValidationStatusValidator;
import org.sagebionetworks.bridge.validators.Validate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * This class represents upload validation status and messages. It's created from an
 * {@link org.sagebionetworks.bridge.models.upload.Upload} object and is returned to users.
 */
@JsonDeserialize(builder = UploadValidationStatus.Builder.class)
public class UploadValidationStatus implements BridgeEntity {
    private final String id;
    private final @Nonnull List<String> messageList;
    private final UploadStatus status;
    private final HealthDataRecord record;

    /** Private constructor. All construction should go through the builder or through the from() methods. */
    private UploadValidationStatus(String id, @Nonnull List<String> messageList, UploadStatus status,
            HealthDataRecord record) {
        this.id = id;
        this.status = status;
        this.record = record;

        // no need to create a safe copy of the message list, since the builder will do that for us
        this.messageList = messageList;
    }

    /**
     * Constructs and validates an UploadValidationStatus from an Upload object.
     *
     * @param upload
     *         Bridge server upload metadata object, must be non-null
     * @param record
     *         corresponding health data record, may be null if it doesn't exist
     * @return validated UploadValidationStatus object
     * @throws InvalidEntityException
     *         if called upload is null or contains invalid fields
     */
    public static UploadValidationStatus from(@Nonnull Upload upload, HealthDataRecord record)
            throws InvalidEntityException {
        if (upload == null) {
            throw new InvalidEntityException(String.format(Validate.CANNOT_BE_NULL, "upload"));
        }
        return new Builder().withId(upload.getUploadId()).withMessageList(upload.getValidationMessageList())
                .withStatus(upload.getStatus()).withRecord(record).build();
    }

    /** Unique upload ID, as generated by the request upload API. */
    public String getId() {
        return id;
    }

    /**
     * Validation message list, frequently used for error messages.
     *
     * @see org.sagebionetworks.bridge.models.upload.Upload#getValidationMessageList
     */
    public @Nonnull List<String> getMessageList() {
        return messageList;
    }

    /** Represents upload status, such as requested, validation in progress, validation failed, or succeeded. */
    public UploadStatus getStatus() {
        return status;
    }

    /** Get the corresponding health data record, if one was created for this upload. */
    public HealthDataRecord getRecord() {
        return record;
    }

    /** Builder for UploadValidationStatus. */
    public static class Builder {
        private String id;
        private List<String> messageList;
        private UploadStatus status;
        private HealthDataRecord record;

        /** @see org.sagebionetworks.bridge.models.upload.UploadValidationStatus#getId */
        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        /** @see org.sagebionetworks.bridge.models.upload.UploadValidationStatus#getMessageList */
        public Builder withMessageList(List<String> messageList) {
            this.messageList = messageList;
            return this;
        }

        /** @see org.sagebionetworks.bridge.models.upload.UploadValidationStatus#getStatus */
        public Builder withStatus(UploadStatus status) {
            this.status = status;
            return this;
        }

        /** @see org.sagebionetworks.bridge.models.upload.UploadValidationStatus#getRecord */
        public Builder withRecord(HealthDataRecord record) {
            this.record = record;
            return this;
        }

        /**
         * Builds and validates an UploadValidationStatus object. id must be non-null and non-empty. messageList must
         * be non-null and must contain strings that are non-null and non-empty. status must be non-null.
         *
         * @return a validated UploadValidationStatus instance
         * @throws InvalidEntityException
         *         if called with invalid fields
         */
        public UploadValidationStatus build() throws InvalidEntityException {
            // Validate messageList. We need to do this upfront, since ImmutableList will crash if this is invalid.
            // We also can't use Validate.entityThrowingException(), since that only works with BridgeEntities.
            if (messageList == null) {
                throw new InvalidEntityException(String.format(Validate.CANNOT_BE_NULL, "messageList"));
            }
            int numMessages = messageList.size();
            for (int i = 0; i < numMessages; i++) {
                if (Strings.isNullOrEmpty(messageList.get(i))) {
                    throw new InvalidEntityException(String.format(Validate.CANNOT_BE_BLANK,
                            String.format("messageList[%d]", i)));
                }
            }

            // create a safe immutable copy of the message list
            List<String> messageListCopy = ImmutableList.copyOf(messageList);

            UploadValidationStatus validationStatus = new UploadValidationStatus(id, messageListCopy, status, record);
            Validate.entityThrowingException(UploadValidationStatusValidator.INSTANCE, validationStatus);
            return validationStatus;
        }
    }
}