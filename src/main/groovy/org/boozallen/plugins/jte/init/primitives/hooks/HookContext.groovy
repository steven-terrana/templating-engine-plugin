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
package org.boozallen.plugins.jte.init.primitives.hooks

import com.google.common.base.Predicate
import org.jenkinsci.plugins.workflow.actions.WarningAction
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.graph.FlowNode
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner

import javax.annotation.Nullable

/**
 * encapsulates the runtime context to inform lifecycle hook annotated library step methods
 */
class HookContext implements Serializable{

    private static final long serialVersionUID = 1L

    /**
     * the library contributing the step that triggered the lifecycle hook
     * <p>
     * {@code null} prior to and post pipeline template execution
     */
    String library

    /**
     * the name of the step that triggered the lifecycle hook
     * <p>
     * {@code null} prior to and post pipeline template execution
     */
    String step

    /**
     * Determines the current status of the pipeline
     */
    HookStatus hookStatus = new HookStatus()

    @SuppressWarnings("EqualsAndHashCode")
    static class HookStatus implements Serializable{

        private static final long serialVersionUID = 1L

        // null unless FAILURE
        Exception exception

        HookStatus(Exception e = null){
            exception = e
        }

        boolean equals(Object o){
            switch(o){
                case "SUCCESS": // successful if there are no messages or exception
                    return !exception && !getWarnings()
                case "FAILURE": // if there's an exception the step failed
                    return exception as boolean
                case "UNSTABLE": // unstable if there are warnings but no exception
                    return getWarnings() && !exception
                case o instanceof HookStatus:
                    HookStatus obj = o as HookStatus
                    return (this.getException() == obj.getException() && this.getWarnings() == obj.getWarnings())
                default:
                    return false
            }
        }

        List<String> getWarnings(){
            // make sure we're in a pipeline
            CpsThread thread = CpsThread.current()
            if(!thread){
                throw new Exception("CpsThread not present.")
            }
            // find all the steps that threw a warning
            ForkScanner scanner = new ForkScanner()
            List<FlowNode> nodes = scanner.filteredNodes(thread.getExecution(), new Predicate<FlowNode>(){
                @Override
                boolean apply(@Nullable FlowNode input) {
                    return input.getAction(WarningAction) as boolean
                }
            })
            // aggregate the warning messages
            List<String> warnings = []
            nodes.collect(warnings){ node ->
                node.getAction(WarningAction).getMessage()
            }
            return warnings
        }
    }

}
