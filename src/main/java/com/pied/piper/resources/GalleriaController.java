package com.pied.piper.resources;

import com.google.inject.Inject;
import com.pied.piper.core.dto.ProfileDetails;
import com.pied.piper.core.dto.SaveImageRequestDto;
import com.pied.piper.core.dto.SearchResponseDto;
import com.pied.piper.core.services.interfaces.GalleriaService;
import com.pied.piper.exception.ResponseException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;

/**
 * Created by akshay.kesarwan on 21/05/16.
 */
@Path("/galleria")
@Produces(MediaType.APPLICATION_JSON)
public class GalleriaController {

    private GalleriaService galleriaService;

    @Inject
    public GalleriaController(GalleriaService galleriaService) {
        this.galleriaService = galleriaService;
    }

    @GET
    @Path("/test")
    public Response test() {
        String result = "Testing Successful";
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveImage(SaveImageRequestDto saveImageRequestDto) {
        HashMap response = new HashMap();
        try {
            Long imageId = galleriaService.saveImage(saveImageRequestDto);
            response.put("image_id", imageId);
        } catch (Exception e) {
            return Response.status(500).build();
        }
        return Response.status(200).entity(response).build();
    }

    /*
        Search Page API
        Provides matching tags and users
     */
    @GET
    @Path("/search/any/{txt}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response search(@PathParam("txt") String searchText) {
        try {
            SearchResponseDto searchResponseDto = galleriaService.search(searchText);
            return Response.status(200).entity(searchResponseDto).build();
        } catch (Exception e) {
            return Response.status(500).build();
        }
    }

    /*
        Profile Details API
    */
    @GET
    @Path("/profile/details/{account_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getProfileDetails(@PathParam("account_id") String accountId) {
        try {
            ProfileDetails profileDetails = galleriaService.getProfileDetails(accountId);
            return Response.status(200).entity(profileDetails).build();
        } catch (ResponseException e) {
            return Response.status(e.getErrorResponse().getErrorCode()).entity(e.getErrorResponse()).build();
        } catch (Exception e) {
            return Response.status(500).build();
        }
    }

    /*
        Clone Image API
    */
    @POST
    @Path("/clone")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cloneImage(@QueryParam("account_id") String accountId, @QueryParam("image_id") Long imageId) {
        HashMap response = new HashMap();
        try {
            Long clonedImageId = galleriaService.cloneImage(imageId, accountId);
            response.put("image_id", clonedImageId);
            return Response.status(200).entity(response).build();
        } catch (ResponseException e) {
            return Response.status(e.getErrorResponse().getErrorCode()).entity(e.getErrorResponse()).build();
        } catch (Exception e) {
            return Response.status(500).build();
        }
    }

    /*
       Send Pull Request API
    */
    @POST
    @Path("/pullrequest/send")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendPullRequest(@QueryParam("account_id") String accountId, @QueryParam("image_id") Long imageId) {
        HashMap response = new HashMap();
        try {
            /*
            Long clonedImageId = galleriaService.cloneImage(imageId, accountId);
            response.put("image_id", clonedImageId);
            return Response.status(200).entity(response).build();
            */
            return Response.status(200).build();
        } catch (ResponseException e) {
            return Response.status(e.getErrorResponse().getErrorCode()).entity(e.getErrorResponse()).build();
        } catch (Exception e) {
            return Response.status(500).build();
        }
    }
}
