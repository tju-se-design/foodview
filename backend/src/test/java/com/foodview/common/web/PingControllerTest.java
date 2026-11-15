package com.foodview.common.web;

import com.foodview.common.api.ErrorCode;
import com.foodview.common.exception.BizException;
import com.foodview.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PingControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PingController(), new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void pingWrapsThePayloadInTheEnvelope() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.status").value("up"));
    }

    @Test
    void bizExceptionKeepsItsStatusCodeAndEnvelope() throws Exception {
        mockMvc.perform(get("/failing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("菜品不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void noResourceIsNotFound() throws Exception {
        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @RestController
    static class FailingController {

        @GetMapping("/failing")
        void failing() {
            throw new BizException(ErrorCode.NOT_FOUND, "菜品不存在");
        }

        @GetMapping("/missing")
        void missing() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "nothing-here", "/missing");
        }
    }
}
