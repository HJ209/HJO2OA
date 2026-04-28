package com.hjo2oa.wf.process.definition.application;

import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;

public interface ProcessDefinitionEngineGateway {

    void deploy(ProcessDefinition definition);

    void delete(ProcessDefinition definition);

    static ProcessDefinitionEngineGateway noop() {
        return NoopProcessDefinitionEngineGateway.INSTANCE;
    }

    enum NoopProcessDefinitionEngineGateway implements ProcessDefinitionEngineGateway {
        INSTANCE;

        @Override
        public void deploy(ProcessDefinition definition) {
        }

        @Override
        public void delete(ProcessDefinition definition) {
        }
    }
}
