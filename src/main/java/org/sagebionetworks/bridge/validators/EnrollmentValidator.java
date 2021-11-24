package org.sagebionetworks.bridge.validators;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sagebionetworks.bridge.validators.Validate.CANNOT_BE_NULL_OR_EMPTY;
import static org.sagebionetworks.bridge.validators.ValidatorUtils.validateStringForPersistence;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

import org.sagebionetworks.bridge.models.studies.Enrollment;

public class EnrollmentValidator extends AbstractValidator {
    public static final EnrollmentValidator INSTANCE = new EnrollmentValidator();
    
    @Override
    public void validate(Object target, Errors errors) {
        Enrollment enrollment = (Enrollment)target;

        if (StringUtils.isBlank(enrollment.getAppId())) {
            errors.rejectValue("appId", CANNOT_BE_NULL_OR_EMPTY);
        }
        if (StringUtils.isBlank(enrollment.getAccountId())) {
            errors.rejectValue("userId", CANNOT_BE_NULL_OR_EMPTY);
        }
        if (StringUtils.isBlank(enrollment.getStudyId())) {
            errors.rejectValue("studyId", CANNOT_BE_NULL_OR_EMPTY);
        }
        if (enrollment.getExternalId() != null && isBlank(enrollment.getExternalId())) {
            errors.rejectValue("externalId", "cannot be blank");
        }
        validateStringForPersistence(errors, 255, enrollment.getEnrolledBy(), "enrolledBy");
        validateStringForPersistence(errors, 255, enrollment.getWithdrawnBy(), "withdrawnBy");
        validateStringForPersistence(errors, 255, enrollment.getWithdrawalNote(), "withdrawalNote");
        // TODO: change to TEXT size for note field
        validateStringForPersistence(errors, 10, enrollment.getNote(), "note");
    }

}
