package dev.morphia.query.validation;

import dev.morphia.mapping.MappedField;

import java.util.List;

import static dev.morphia.query.validation.ValueClassValidator.valueIsClassOrSubclassOf;
import static java.lang.String.format;

/**
 * Checks if the value can have the {@code FilterOperator.ALL} operator applied to it.
 */
@SuppressWarnings("removal")
public final class SizeOperationValidator extends OperationValidator {
    private static final SizeOperationValidator INSTANCE = new SizeOperationValidator();

    private SizeOperationValidator() {
    }

    /**
     * Get the instance
     *
     * @return the Singleton instance of this validator
     */
    public static SizeOperationValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected dev.morphia.query.FilterOperator getOperator() {
        return dev.morphia.query.FilterOperator.SIZE;
    }

    @Override
    protected void validate(final MappedField mappedField, final Object value,
                            final List<ValidationFailure> validationFailures) {
        if (!valueIsClassOrSubclassOf(value, Number.class)) {
            validationFailures.add(new ValidationFailure(format("For a $size operation, value '%s' should be an integer type.  "
                                                                + "Instead it was a: %s", value, value.getClass())));

        }
        if (!CollectionTypeValidator.typeIsIterableOrArrayOrMap(mappedField.getType())) {
            validationFailures.add(new ValidationFailure(format("For a $size operation, field '%s' should be a List or array.  "
                                                                + "Instead it was a: %s",
                                                                mappedField, mappedField.getType())));
        }
    }
}
