/*
    Copyright 2018 Booz Allen Hamilton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package org.boozallen.plugins.jte.init.primitives.injectors

import hudson.Extension
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * checks for collisions between TemplateBinding primitives and Jenkins globals and steps
 */
@Extension class GlobalCollisionValidator extends TemplatePrimitiveInjector{

    static String warningHeading = "There are JTE Primitives that have naming collisions with Jenkins globals and/or steps"
    static String warningMsg = " has a Jenkins global/step collision"

    @Override
    void validateBinding(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding) {
        List<String> collisions = checkPrimitiveCollisions( binding, flowOwner.getExecutable())

        if( collisions ){
            List<String> warnings = [
                    warningHeading,
            ]
            collisions.each{ name ->
                warnings << "- ${name}: ${warningMsg}"
            }

            TemplateLogger logger = new TemplateLogger(flowOwner.getListener())
            logger.print(warnings.join("\n"))
        }
    }

    // will probably become a method on the validation class
    List<String> checkPrimitiveCollisions(TemplateBinding templateBinding, hudson.model.Run run){
        Set<String> registry = templateBinding.getPrimitiveNames()
        List<String> functionNames = org.jenkinsci.plugins.workflow.steps.StepDescriptor.all()*.functionName
        Set<String> collisions = registry.intersect(functionNames)

        collisions += registry.collect { key ->
            org.jenkinsci.plugins.workflow.cps.GlobalVariable.byName(key, run)
        }.findAll{ g -> null != g }

        return new ArrayList<String>(collisions as Collection<String>)
    }

}
