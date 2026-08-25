import { getApiError, getErrorMessage, getFieldErrors } from "@/lib/api";

/**
 * Form-level view of a failed submit.
 *
 * `fieldErrors` holds only messages whose `field` the form actually renders;
 * `formError` carries everything else. Contract §6.5: a field name the form does
 * not recognize must surface as a form-level message rather than being silently
 * swallowed, so a new backend validation rule can never make a submit look like
 * a no-op.
 */
export interface FormErrorState {
  fieldErrors: Record<string, string>;
  formError: string | null;
}

export const EMPTY_FORM_ERRORS: FormErrorState = {
  fieldErrors: {},
  formError: null,
};

/**
 * Split an API failure into per-field messages and a form-level message.
 *
 * @param error       the thrown value from an axios call
 * @param knownFields field names the form renders an inline message for
 * @param aliases     maps a non-validation error code onto a field, so e.g.
 *                    `409 EMAIL_ALREADY_REGISTERED` lands under the email input
 */
export function toFormErrors(
  error: unknown,
  knownFields: readonly string[],
  aliases: Record<string, string> = {},
): FormErrorState {
  const message = getErrorMessage(error);
  const code = getApiError(error)?.code ?? null;

  const aliasField = code ? aliases[code] : undefined;
  if (aliasField) {
    return { fieldErrors: { [aliasField]: message }, formError: null };
  }

  const fieldErrors: Record<string, string> = {};
  let hasUnmapped = false;
  for (const [field, fieldMessage] of Object.entries(getFieldErrors(error))) {
    if (knownFields.includes(field)) {
      fieldErrors[field] = fieldMessage;
    } else {
      hasUnmapped = true;
    }
  }

  const mappedCount = Object.keys(fieldErrors).length;
  // Show the envelope message unless every violation found an inline home.
  const formError = mappedCount > 0 && !hasUnmapped ? null : message;

  return { fieldErrors, formError };
}
