package com.example.rest;

import com.example.model.MergedPost;
import com.example.service.PostService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
public class PostsResource {

    @Inject
    PostService postService;

    @GET
    public List<MergedPost> getPosts(
            @QueryParam("limit") @Min(1) @Max(100) Integer limit,
            @QueryParam("includeComments") Boolean includeComments,
            @QueryParam("userId") Integer userId
    ) {
        boolean include = includeComments == null ? true : includeComments;
        return postService.getMergedPosts(limit, include, userId);
    }
}
