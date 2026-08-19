package com.example.demo.eval;

import java.util.List;

/**
 * The result of running one {@link ContractEvalSample} through the real extraction
 * pipeline. {@code aiAvailable=false} means no AI provider was configured for this run
 * (fields list is empty) - not a failure of the sample or the pipeline, just nothing to
 * grade. {@code error} is set instead of {@code fields} when the PDF itself couldn't be
 * read.
 */
public record ContractEvalSampleResult(
        Long sampleId,
        String fileName,
        boolean aiAvailable,
        List<ContractEvalFieldResult> fields,
        String error
) {
}
