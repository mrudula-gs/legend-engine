// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.application.query.api;

import com.mongodb.client.MongoClient;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.application.query.model.DataCubeQuery;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QuerySearchSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(tags = "Application - Query")
@Path("pure/v1/query")
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationQuery
{
    private final QueryStoreManager queryStoreManager;
    private final DataCubeQueryStoreManager dataCubeQueryStoreManager;

    public ApplicationQuery(MongoClient mongoClient)
    {
        this.queryStoreManager = new QueryStoreManager(mongoClient);
        this.dataCubeQueryStoreManager = new DataCubeQueryStoreManager(mongoClient);
    }

    private static String getCurrentUser(ProfileManager<CommonProfile> profileManager)
    {
        CommonProfile profile = profileManager.get(true).orElse(null);
        return profile != null ? profile.getId() : null;
    }

    @POST
    @Path("search")
    @ApiOperation(value = "Search queries")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response searchQueries(QuerySearchSpecification searchSpecification, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try
        {
            return Response.ok().entity(this.queryStoreManager.searchQueries(searchSpecification, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.SEARCH_QUERIES_ERROR, null);
        }
    }

    @GET
    @Path("batch")
    @ApiOperation(value = "Get the queries with specified IDs")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueries(@QueryParam("queryIds") @ApiParam("The list of query IDs to fetch (must contain no more than 50 items)") List<String> queryIds)
    {
        try
        {
            return Response.ok(this.queryStoreManager.getQueries(queryIds)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERIES_ERROR, null);
        }
    }

    @GET
    @Path("{queryId}")
    @ApiOperation(value = "Get the query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQuery(@PathParam("queryId") String queryId)
    {
        try
        {
            return Response.ok(this.queryStoreManager.getQuery(queryId)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_ERROR, null);
        }
    }

    @POST
    @ApiOperation(value = "Create a new query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createQuery(Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Create Query").startActive(true))
        {
            return Response.ok().entity(this.queryStoreManager.createQuery(query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.CREATE_QUERY_ERROR, identity.getName());
        }
    }

    @PUT
    @Path("{queryId}")
    @ApiOperation(value = "Update query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateQuery(@PathParam("queryId") String queryId, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Update Query").startActive(true))
        {
            return Response.ok().entity(this.queryStoreManager.updateQuery(queryId, query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, identity.getName());
        }
    }

    @PUT
    @Path("{queryId}/patchQuery")
    @ApiOperation(value = "Patch Query - update selected query fields")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response patchQuery(@PathParam("queryId") String queryId, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Patch Query - update selected query fields").startActive(true))
        {
            return Response.ok().entity(this.queryStoreManager.patchQuery(queryId, query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, identity.getName());
        }
    }

    @DELETE
    @Path("{queryId}")
    @ApiOperation(value = "Delete the query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteQuery(@PathParam("queryId") String queryId, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Query: Delete Query").startActive(true))
        {
            this.queryStoreManager.deleteQuery(queryId, getCurrentUser(profileManager));
            return Response.noContent().build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.DELETE_QUERY_ERROR, identity.getName());
        }
    }

    @GET
    @Path("events")
    @ApiOperation(value = "Get query events")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueryEvents(@QueryParam("queryId") @ApiParam("The query ID the event is associated with") String queryId,
                                   @QueryParam("eventType") @ApiParam("The type of event") QueryEvent.QueryEventType eventType,
                                   @QueryParam("since") @ApiParam("Lower limit on the UNIX timestamp for the event creation time") Long since,
                                   @QueryParam("until") @ApiParam("Upper limit on the UNIX timestamp for the event creation time") Long until,
                                   @QueryParam("limit") @ApiParam("Limit the number of events returned") Integer limit,
                                   @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try
        {
            return Response.ok().entity(this.queryStoreManager.getQueryEvents(queryId, eventType, since, until, limit)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_EVENTS_ERROR, null);
        }
    }

    @GET
    @Path("/stats")
    @ApiOperation(value = "Get query store statistics")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQueryStoreStats()
    {
        try
        {
            return Response.ok(this.queryStoreManager.getQueryStoreStats()).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_STATS_ERROR, null);
        }
    }

    // --------------------------------------------- DataCube Query ---------------------------------------------

    @POST
    @Path("dataCube/search")
    @ApiOperation(value = "Search DataCube queries")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response searchDataCubeQueries(QuerySearchSpecification searchSpecification, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try
        {
            return Response.ok().entity(this.dataCubeQueryStoreManager.searchQueries(searchSpecification, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.SEARCH_QUERIES_ERROR, null);
        }
    }

    @GET
    @Path("dataCube/batch")
    @ApiOperation(value = "Get the DataCube queries with specified IDs")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDataCubeQueries(@QueryParam("queryIds") @ApiParam("The list of query IDs to fetch (must contain no more than 50 items)") List<String> queryIds)
    {
        try
        {
            return Response.ok(this.dataCubeQueryStoreManager.getQueries(queryIds)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERIES_ERROR, null);
        }
    }

    @GET
    @Path("dataCube/{queryId}")
    @ApiOperation(value = "Get the DataCube query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDataCubeQuery(@PathParam("queryId") String queryId)
    {
        try
        {
            return Response.ok(this.dataCubeQueryStoreManager.getQuery(queryId)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_ERROR, null);
        }
    }

    @POST
    @Path("dataCube")
    @ApiOperation(value = "Create a new DataCube query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createDataCubeQuery(DataCubeQuery query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            return Response.ok().entity(this.dataCubeQueryStoreManager.createQuery(query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.CREATE_QUERY_ERROR, identity.getName());
        }
    }

    @PUT
    @Path("dataCube/{queryId}")
    @ApiOperation(value = "Update DataCube query")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateDataCubeQuery(@PathParam("queryId") String queryId, DataCubeQuery query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            return Response.ok().entity(this.dataCubeQueryStoreManager.updateQuery(queryId, query, getCurrentUser(profileManager))).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.UPDATE_QUERY_ERROR, identity.getName());
        }
    }

    @DELETE
    @Path("dataCube/{queryId}")
    @ApiOperation(value = "Delete the DataCube query with specified ID")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteDataCubeQuery(@PathParam("queryId") String queryId, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            this.dataCubeQueryStoreManager.deleteQuery(queryId, getCurrentUser(profileManager));
            return Response.noContent().build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.DELETE_QUERY_ERROR, identity.getName());
        }
    }

    @GET
    @Path("dataCube/events")
    @ApiOperation(value = "Get DataCube query events")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDataCubeQueryEvents(@QueryParam("queryId") @ApiParam("The query ID the event is associated with") String queryId,
                                           @QueryParam("eventType") @ApiParam("The type of event") QueryEvent.QueryEventType eventType,
                                           @QueryParam("since") @ApiParam("Lower limit on the UNIX timestamp for the event creation time") Long since,
                                           @QueryParam("until") @ApiParam("Upper limit on the UNIX timestamp for the event creation time") Long until,
                                           @QueryParam("limit") @ApiParam("Limit the number of events returned") Integer limit,
                                           @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        try
        {
            return Response.ok().entity(this.dataCubeQueryStoreManager.getQueryEvents(queryId, eventType, since, until, limit)).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_EVENTS_ERROR, null);
        }
    }

    @GET
    @Path("dataCube/stats")
    @ApiOperation(value = "Get DataCube query store statistics")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDataCubeQueryStoreStats()
    {
        try
        {
            return Response.ok(this.dataCubeQueryStoreManager.getQueryStoreStats()).build();
        }
        catch (Exception e)
        {
            if (e instanceof ApplicationQueryException)
            {
                return ((ApplicationQueryException) e).toResponse();
            }
            return ExceptionTool.exceptionManager(e, LoggingEventType.GET_QUERY_STATS_ERROR, null);
        }
    }
}
