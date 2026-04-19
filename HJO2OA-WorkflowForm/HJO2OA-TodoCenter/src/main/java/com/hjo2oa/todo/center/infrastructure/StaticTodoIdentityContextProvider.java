package com.hjo2oa.todo.center.infrastructure;

import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import org.springframework.stereotype.Component;

@Component
public class StaticTodoIdentityContextProvider implements TodoIdentityContextProvider {

    @Override
    public TodoIdentityContext currentContext() {
        return new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
    }
}
