
package blogpad.reactor.posts.boundary;

import blogpad.reactor.posts.control.PostsFetcher;
import blogpad.reactor.posts.control.Templates;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 *
 * @author airhacks.com
 */
@ApplicationScoped
public class Reactor {

    private Source handlebars;
    private Source spg;

    @Inject
    @RestClient
    PostsFetcher posts;

    @Inject
    @RestClient
    Templates templates;

    @Inject
    @ConfigProperty(name = "single.post.template", defaultValue = "post.html")
    String singlePostTemplate;

    @Inject
    @ConfigProperty(name = "post.list.template", defaultValue = "list.html")
    String postListTemplate;

    @PostConstruct
    public void init() {
        try {
            this.handlebars = Source.newBuilder("js", loadScriptFromJar("handlebars-v4.7.6.js"), "Handlebars").build();
            this.spg = Source.newBuilder("js", loadScriptFromJar("spg.js"), "spg").build();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load scripts: " + ex.getMessage());
        }
    }

    @Timed
    public String react(String title) {
        String stringifedPost = this.posts.getPostByTitle(title);
        String template = this.getSinglePostTemplate();
        try {
            return this.react(template, stringifedPost);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create content " + ex);
        }
    }

    String getSinglePostTemplate() {
        JsonObject template = this.templates.getTemplateByName(singlePostTemplate);
        return template.getString("content");
    }

    public String react(String templateContent, String parameterContentAsJSON) throws IOException {
        try ( Context context = Context.create("js")) {
            Value bindings = context.getBindings("js");
            context.eval(this.handlebars);
            bindings.putMember("templateContent", templateContent);
            bindings.putMember("parameterContent", parameterContentAsJSON);
            return context.eval(this.spg).asString();
        }
    }

    static Reader loadScriptFromJar(String fileName) {
        System.out.printf("Loading script %s%n", fileName);
        InputStream stream = Thread.currentThread().
                getContextClassLoader().
                getResourceAsStream("js/" + fileName);
        if (stream == null) {
            throw new IllegalStateException("Cannot load: " + fileName);
        }
        return new InputStreamReader(stream);
    }
}
