
package airhacks;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author airhacks.com
 */
@Path("posts")
public interface PostsResourceClient {

    @GET
    @Path("{title}")
    @Produces(MediaType.TEXT_HTML)
    Response fetchPost(@PathParam("title") String title);
}
