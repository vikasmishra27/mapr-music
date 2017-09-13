package com.mapr.music.api;


import com.mapr.music.dao.SortOption;
import com.mapr.music.dto.AlbumDto;
import com.mapr.music.dto.ResourceDto;
import com.mapr.music.dto.TrackDto;
import com.mapr.music.model.Album;
import com.mapr.music.service.AlbumService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

/**
 * Endpoint for accessing 'Album' resources.
 */
@Api(value = AlbumEndpoint.ENDPOINT_PATH, description = "Albums endpoint, which allows to manage 'Album' documents")
@Path(AlbumEndpoint.ENDPOINT_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AlbumEndpoint {

    public static final String ENDPOINT_PATH = "/albums";

    @Inject
    private AlbumService albumService;

    @GET
    @Path("{id}")
    @ApiOperation(value = "Get single album by it's identifier")
    public AlbumDto getAlbum(@ApiParam(value = "Album's identifier", required = true) @PathParam("id") String id) {
        return albumService.getAlbumById(id);
    }

    @GET
    @Path("/slug/{slug}")
    @ApiOperation(value = "Get single album by it's slug name")
    public AlbumDto getAlbumBySlugName(@ApiParam(value = "Slug name", required = true) @PathParam("slug") String slug) {
        return albumService.getAlbumBySlugName(slug);
    }

    @GET
    @Path("/")
    @ApiOperation(value = "Get list of albums, which is represented by page")
    public ResourceDto<AlbumDto> getAlbumsPage(@QueryParam("per_page") Long perPage,
                                               @QueryParam("page") Long page,
                                               @QueryParam("sort") List<SortOption> sortOptions,
                                               @QueryParam("language") String language) {

        if (language != null) {
            return albumService.getAlbumsPageByLanguage(perPage, page, sortOptions, language);
        }

        return albumService.getAlbumsPage(perPage, page, sortOptions);
    }

    @DELETE
    @Path("{id}")
    @ApiOperation(value = "Delete single album by it's identifier")
    @RolesAllowed("ADMIN")
    public void deleteAlbum(@PathParam("id") String id) {
        albumService.deleteAlbumById(id);
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update single album")
    @RolesAllowed("ADMIN")
    public AlbumDto updateAlbum(@PathParam("id") String id, Album album) {
        return albumService.updateAlbum(id, album);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create album")
    @RolesAllowed("ADMIN")
    public Response createAlbum(Album album, @Context UriInfo uriInfo) {

        AlbumDto createdAlbum = albumService.createAlbum(album);
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path(createdAlbum.getId());
        URI location = builder.build();

        return Response.status(Response.Status.CREATED).entity(createdAlbum).location(location).build();
    }

    @GET
    @Path("{id}/tracks")
    @ApiOperation(value = "Get list of album's tracks")
    public List<TrackDto> getAlbumTracks(@PathParam("id") String id) {
        return albumService.getAlbumTracksList(id);
    }

    @GET
    @Path("{album-id}/tracks/{track-id}")
    @ApiOperation(value = "Get single album's track")
    public TrackDto getAlbumsSingleTrack(@PathParam("album-id") String albumId,
                                      @PathParam("track-id") String trackId) {

        return albumService.getTrackById(albumId, trackId);
    }

    @PUT
    @Path("{id}/tracks")
    @ApiOperation(value = "Updates the whole list of album's tracks")
    @RolesAllowed("ADMIN")
    public List<TrackDto> setAlbumTracks(@PathParam("id") String id, List<TrackDto> trackList) {
        return albumService.setAlbumTrackList(id, trackList);
    }

    @PUT
    @Path("{album-id}/tracks/{track-id}")
    @ApiOperation(value = "Update single album's track")
    @RolesAllowed("ADMIN")
    public TrackDto updateAlbumsSingleTrack(@PathParam("album-id") String albumId,
                                         @PathParam("track-id") String trackId, TrackDto trackDto) {

        return albumService.updateAlbumTrack(albumId, trackId, trackDto);
    }

    @DELETE
    @Path("{album-id}/tracks/{track-id}")
    @ApiOperation(value = "Delete single album's track")
    @RolesAllowed("ADMIN")
    public void updateAlbumsSingleTrack(@PathParam("album-id") String albumId, @PathParam("track-id") String trackId) {
        albumService.deleteAlbumTrack(albumId, trackId);
    }

    @POST
    @Path("{id}/tracks/")
    @ApiOperation(value = "Create single album's track")
    @RolesAllowed("ADMIN")
    public TrackDto createAlbumsSingleTrack(@PathParam("id") String id, TrackDto trackDto) {
        return albumService.addTrackToAlbumTrackList(id, trackDto);
    }
}