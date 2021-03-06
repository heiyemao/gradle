/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.binarycompatibility.rules;

import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibility;
import japicmp.model.JApiHasAnnotations;
import me.champeau.gradle.japicmp.report.Violation;

import java.util.Map;

public class BinaryBreakingChangesRule extends AbstractGradleViolationRule {

    public BinaryBreakingChangesRule(Map<String, String> acceptedApiChanges) {
        super(acceptedApiChanges);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Violation maybeViolation(final JApiCompatibility member) {
        if (!member.isBinaryCompatible()) {
            if ((member instanceof JApiClass) && (member.getCompatibilityChanges().isEmpty())) {
                // A member of the class breaks binary compatibility.
                // That will be handled when the member is passed to `maybeViolation`.
                return null;
            }
            if (member instanceof JApiHasAnnotations) {
                if (isIncubating((JApiHasAnnotations) member)) {
                    return Violation.accept(member, "Removed member was incubating");
                }
            }

            return acceptOrReject(member, Violation.notBinaryCompatible(member));
        }
        return null;
    }

}
