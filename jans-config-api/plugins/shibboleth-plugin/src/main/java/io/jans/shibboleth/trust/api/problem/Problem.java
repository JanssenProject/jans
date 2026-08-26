package io.jans.shibboleth.trust.api.problem;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Error response body in the RFC 7807 {@code application/problem+json} format, shared by every trust
 * API. {@code type}, {@code title}, {@code status} and {@code code} are always present; {@code code}
 * is the stable, machine-readable discriminator. {@code detail}, {@code instance} and
 * {@code violations} are optional and omitted when absent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Problem {

    @JsonProperty("type")
    private final String type;

    @JsonProperty("title")
    private final String title;

    @JsonProperty("status")
    private final int status;

    @JsonProperty("detail")
    private final String detail;

    @JsonProperty("instance")
    private final String instance;

    @JsonProperty("code")
    private final String code;

    @JsonProperty("violations")
    private final List<Violation> violations;

    @JsonCreator
    public Problem(
            @JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("status") int status,
            @JsonProperty("detail") String detail,
            @JsonProperty("instance") String instance,
            @JsonProperty("code") String code,
            @JsonProperty("violations") List<Violation> violations) {

        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.code = code;
        this.violations = violations;
    }

    public String getType() {

        return type;
    }

    public String getTitle() {

        return title;
    }

    public int getStatus() {

        return status;
    }

    public String getDetail() {

        return detail;
    }

    public String getInstance() {

        return instance;
    }

    public String getCode() {

        return code;
    }

    public List<Violation> getViolations() {

        return violations;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (o == null || getClass() != o.getClass()) {

            return false;
        }
        Problem that = (Problem) o;
        return status == that.status
            && Objects.equals(type, that.type)
            && Objects.equals(title, that.title)
            && Objects.equals(detail, that.detail)
            && Objects.equals(instance, that.instance)
            && Objects.equals(code, that.code)
            && Objects.equals(violations, that.violations);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, title, status, detail, instance, code, violations);
    }

    @Override
    public String toString() {

        return "Problem{type='" + type + "', title='" + title + "', status=" + status
            + ", detail='" + detail + "', instance='" + instance + "', code='" + code
            + "', violations=" + violations + "}";
    }
}
