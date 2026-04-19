package com.hjo2oa.msg.message.center.infrastructure;

import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContextProvider;
import org.springframework.stereotype.Component;

@Component
public class StaticMessageIdentityContextProvider implements MessageIdentityContextProvider {

    @Override
    public MessageIdentityContext currentContext() {
        return new MessageIdentityContext(
                "tenant-1",
                "assignment-1",
                "assignment-1",
                "position-1"
        );
    }
}
