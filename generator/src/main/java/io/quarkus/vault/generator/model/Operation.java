package io.quarkus.vault.generator.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.quarkus.vault.generator.errors.MissingParameterError;
import io.quarkus.vault.generator.utils.Strings;

public record Operation(
        @JsonProperty(required = true) String name,
        Optional<String> trace,
        Optional<Method> method,
        Optional<Status> status,
        Optional<String> path,
        Optional<PathChoice> pathChoice,
        Optional<List<Parameter>> parameters,
        Optional<Boolean> authenticated,
        Optional<String> tokenFrom,
        Optional<String> wrapTTLFrom,
        Optional<List<String>> bodyFrom,
        Optional<String> bodyType,
        Optional<List<String>> queryFrom,
        Optional<Map<String, String>> headers,
        Optional<Result> result) {

    public enum Method {
        GET(false),
        POST(true),
        PUT(true),
        PATCH(true),
        DELETE(false),
        HEAD(false),
        LIST(false);

        public final boolean allowsBody;

        public String getBuilderMethodName() {
            return name().toLowerCase();
        }

        Method(boolean allowsBody) {
            this.allowsBody = allowsBody;
        }
    }

    public record Parameter(
            @JsonProperty(required = true) String name,
            Optional<String> serializedName,
            Optional<Boolean> required,
            Optional<Boolean> includeNulls,
            Optional<Boolean> body,
            Optional<String> type,
            Optional<List<POJO.Property>> object) {

        public POJO.Property asProperty() {
            return new POJO.Property(name, serializedName, required, type);
        }

        public String getSerializedName() {
            return serializedName.orElseGet(() -> Strings.camelCaseToSnakeCase(name));
        }

        public boolean isRequired() {
            return required.orElse(true);
        }

        public boolean isIncludeNulls() {
            return includeNulls.orElse(true);
        }

        public boolean isTypeImplied() {
            return type.isPresent() || object.isEmpty();
        }

        public String getImpliedType() {
            return type.orElse("java.lang.String");
        }

        public boolean isBody() {
            return body.orElse(false);
        }

    }

    public record PathChoice(
            @JsonProperty(required = true) String param,
            @JsonProperty(required = true) List<Choice> choices) {

        public record Choice(
                @JsonProperty(required = true) Object value,
                @JsonProperty(required = true) String path) {
        }

        public Choice getRequiredChoice(Object value) {
            return choices.stream()
                    .filter(choice -> choice.value.equals(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No choice specified for value " + value));
        }

    }

    public enum Status {
        OK,
        NO_CONTENT,
        ACCEPTED,
        OK_OR_ACCEPTED
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LeasedResult.class),
            @JsonSubTypes.Type(value = JSONResult.class),
            @JsonSubTypes.Type(value = RawResult.class)
    })
    public sealed interface Result {
        enum Kind {
            @JsonProperty("leased")
            Leased,
            @JsonProperty("json")
            JSON,
            @JsonProperty("raw")
            Raw
        }

        Kind kind();
    }

    @JsonTypeName("leased")
    public record LeasedResult(
            Kind kind,
            Optional<Boolean> unwrapsData,
            Optional<List<POJO.Property>> data,
            Optional<String> dataType,
            Optional<List<POJO.Property>> auth,
            Optional<String> authType,
            Optional<Boolean> unwrapsAuth,
            Optional<PartialPOJO> custom) implements Result {

        @Override
        public Kind kind() {
            return Kind.Leased;
        }
    }

    @JsonTypeName("json")
    public record JSONResult(
            Optional<String> type,
            Optional<List<POJO.Property>> object) implements Result {

        @Override
        public Kind kind() {
            return Kind.JSON;
        }
    }

    @JsonTypeName("raw")
    public record RawResult(
            @JsonProperty(required = true) String type) implements Result {

        @Override
        public Kind kind() {
            return Kind.Raw;
        }
    }

    public String getTraceTitle() {
        return trace.orElseGet(() -> Strings.camelCaseToTitle(name));
    }

    public Method getMethod() {
        return method.orElse(Method.GET);
    }

    public Optional<Status> getStatus() {
        if (status.isPresent()) {
            return status;
        }
        return Optional.ofNullable(
                switch (getMethod()) {
                    case GET, LIST, POST -> result.isPresent() ? Status.OK : Status.NO_CONTENT;
                    case PATCH, PUT, DELETE -> Status.NO_CONTENT;
                    case HEAD -> null;
                });
    }

    public boolean isAuthenticated() {
        return authenticated.orElse(true);
    }

    public Optional<Parameter> getBodyParameter() {
        return getParameters().stream()
                .filter(Parameter::isBody)
                .findFirst();
    }

    public Optional<Parameter> getParameter(String name) {
        return getParameters().stream()
                .filter(parameter -> parameter.name().equals(name))
                .findFirst();
    }

    public Parameter getRequiredParameter(String name) {
        return getParameter(name).orElseThrow(() -> MissingParameterError.of(name));
    }

    public List<Parameter> getParameters() {
        return parameters.orElse(List.of());
    }
}
