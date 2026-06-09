package com.slideforge.api.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OpenAiCompatibleProviderClient implements AiProviderClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleProviderClient(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(120_000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return "openai-compatible";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        try {
            OpenAiChatResponse response = restClient.post()
                    .uri(chatCompletionsUrl(request.baseUrl()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toOpenAiRequest(request))
                    .retrieve()
                    .body(OpenAiChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回格式不合法：choices 为空");
            }

            String content = response.choices().getFirst().message().content();

            if (content == null || content.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回内容为空");
            }

            return new AiChatResponse(
                    content,
                    response.usage() == null ? null : response.usage().promptTokens(),
                    response.usage() == null ? null : response.usage().completionTokens(),
                    toJson(response)
            );
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AI Key 无效或无权限");
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 模型或接口不存在");
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 服务请求失败：" + exception.getStatusCode().value()
            );
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 服务不可达或调用超时");
        }
    }

    private String chatCompletionsUrl(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }

        return normalized + "/chat/completions";
    }

    private OpenAiChatRequest toOpenAiRequest(AiChatRequest request) {
        return new OpenAiChatRequest(
                request.model(),
                request.messages(),
                request.temperature(),
                request.maxTokens(),
                "json".equalsIgnoreCase(request.responseFormat())
                        ? Map.of("type", "json_object")
                        : null
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "";
        }
    }

    private record OpenAiChatRequest(
            String model,
            List<AiMessage> messages,
            Double temperature,
            Integer max_tokens,
            Map<String, String> response_format
    ) {
    }

    private record OpenAiChatResponse(
            List<Choice> choices,
            Usage usage
    ) {
    }

    private record Choice(
            AiMessage message
    ) {
    }

    private record Usage(
            Integer prompt_tokens,
            Integer completion_tokens
    ) {
        Integer promptTokens() {
            return prompt_tokens;
        }

        Integer completionTokens() {
            return completion_tokens;
        }
    }
}
