package com.example.rest;

import com.example.model.*;
import com.example.service.PostService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Path("/v2/posts")
@Produces(MediaType.APPLICATION_JSON)
public class PostsResourceV2 {

    @Inject
    PostService postService;

    @GET
    public Response getPosts(
            @QueryParam("page") @Min(1) Integer page,
            @QueryParam("size") @Min(1) @Max(100) Integer size,
            @QueryParam("q") String q,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("order") @DefaultValue("asc") String order,
            @QueryParam("userId") Integer userId,
            @QueryParam("includeComments") @DefaultValue("true") boolean includeComments
    ) {
        List<MergedPost> all = postService.getMergedPosts(null, includeComments, userId);

        // search
        if (q != null && !q.isBlank()) {
            String needle = q.toLowerCase();
            all = all.stream().filter(p ->
                    (p.title != null && p.title.toLowerCase().contains(needle)) ||
                    (p.body != null && p.body.toLowerCase().contains(needle)) ||
                    (p.author != null && p.author.name != null && p.author.name.toLowerCase().contains(needle))
            ).collect(Collectors.toList());
        }

        // sort
        Comparator<MergedPost> cmp = Comparator.comparingInt(p -> p.id);
        if ("title".equalsIgnoreCase(sortBy)) cmp = Comparator.comparing(p -> p.title == null ? "" : p.title);
        else if ("commentCount".equalsIgnoreCase(sortBy)) cmp = Comparator.comparingInt(p -> p.commentCount);
        else if ("author".equalsIgnoreCase(sortBy)) cmp = Comparator.comparing(p -> p.author == null ? "" : p.author.name == null ? "" : p.author.name);
        if ("desc".equalsIgnoreCase(order)) cmp = cmp.reversed();
        all.sort(cmp);

        int total = all.size();
        int pg = page == null ? 1 : page;
        int sz = size == null ? Math.min(20, Math.max(1, total)) : size;
        int from = Math.min((pg - 1) * sz, total);
        int to = Math.min(from + sz, total);
        List<MergedPost> slice = all.subList(from, to);

        return Response.ok(slice)
                .header("X-Total-Count", total)
                .header("X-Page", pg)
                .header("X-Size", sz)
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getPostById(@PathParam("id") int id, @QueryParam("includeComments") @DefaultValue("true") boolean includeComments) {
        List<MergedPost> list = postService.getMergedPosts(null, includeComments, null);
        return list.stream().filter(p -> p.id == id).findFirst()
                .<Response>map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/users/{userId}")
    public List<MergedPost> getPostsByUser(@PathParam("userId") int userId, @QueryParam("includeComments") @DefaultValue("false") boolean includeComments) {
        return postService.getMergedPosts(null, includeComments, userId);
    }

    @GET
    @Path("/stats")
    public Map<String, Object> getStats() {
        List<MergedPost> all = postService.getMergedPosts(null, true, null);
        Map<Integer, Long> byUser = all.stream().collect(Collectors.groupingBy(p -> p.author == null ? -1 : p.author.id, Collectors.counting()));
        int maxComments = all.stream().mapToInt(p -> p.commentCount).max().orElse(0);
        double avgComments = all.stream().mapToInt(p -> p.commentCount).average().orElse(0.0);
        return Map.of(
                "postsTotal", all.size(),
                "postsByUser", byUser,
                "maxCommentsInAPost", maxComments,
                "avgCommentsPerPost", avgComments
        );
    }
}
