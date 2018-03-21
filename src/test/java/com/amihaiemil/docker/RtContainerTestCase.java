/**
 * Copyright (c) 2018, Mihai Emil Andronache
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1)Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2)Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 3)Neither the name of docker-java-api nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.amihaiemil.docker;

import com.amihaiemil.docker.mock.AssertRequest;
import com.amihaiemil.docker.mock.Condition;
import com.amihaiemil.docker.mock.Response;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import javax.json.Json;
import javax.json.JsonObject;
import java.net.URI;

/**
 * Unit tests for RtContainer.
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 0.0.1
 */
public final class RtContainerTestCase {

    /**
     * RtContainer can return its id.
     */
    @Test
    public void returnsId() {
        final Container container = new RtContainer(
            Mockito.mock(HttpClient.class),
            URI.create("unix://localhost:80/1.30/containers/123id456")
        );
        MatcherAssert.assertThat(
            container.containerId(),
            Matchers.equalTo("123id456")
        );
    }

    /**
     * RtContainer can return info about itself.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void inspectsItself() throws Exception {
        final Container container = new RtContainer(
            new AssertRequest(
                new Response(
                    HttpStatus.SC_OK,
                    Json.createObjectBuilder()
                        .add("Id", "123")
                        .add("Image", "some/image")
                        .add("Name", "boring_euclid")
                        .build().toString()
                ),
                new Condition(
                    "Method should be a GET",
                    req -> req.getRequestLine().getMethod().equals("GET")
                ),
                new Condition(
                    "Resource path must be /{id}/json",
                    req -> req.getRequestLine().getUri().endsWith("/123/json")
                )
            ),
            URI.create("http://localhost:80/1.30/containers/123")
        );
        final JsonObject info = container.inspect();
        MatcherAssert.assertThat(info.keySet(), Matchers.hasSize(3));
        MatcherAssert.assertThat(
            info.getString("Id"), Matchers.equalTo("123")
        );
        MatcherAssert.assertThat(
            info.getString("Image"), Matchers.equalTo("some/image")
        );
        MatcherAssert.assertThat(
            info.getString("Name"), Matchers.equalTo("boring_euclid")
        );
    }

    /**
     * RtContainer.inspect() throws ISE because the HTTP Response's status
     * is not 200 OK.
     * @throws Exception If something goes wrong.
     */
    @Test(expected = UnexpectedResponseException.class)
    public void inspectsNotFound() throws Exception {
        new RtContainer(
            new AssertRequest(
                new Response(HttpStatus.SC_NOT_FOUND, "")
            ),
            URI.create("http://localhost:80/1.30/containers/123")
        ).inspect();
    }

    /**
     * RtContainer can start with no problem.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void startsOk() throws Exception {
        new RtContainer(
            new AssertRequest(
                new Response(
                    HttpStatus.SC_NO_CONTENT, ""
                ),
                new Condition(
                    "Method should be a POST",
                    req -> req.getRequestLine().getMethod().equals("POST")
                ),
                new Condition(
                    "Resource path must be /{id}/start",
                    req -> req.getRequestLine().getUri().endsWith("/123/start")
                )
            ),
            URI.create("http://localhost:80/1.30/containers/123")
        ).start();
    }

    /**
     * RtContainer throws ISE if it receives server error on start.
     * @throws Exception If something goes wrong.
     */
    @Test(expected = UnexpectedResponseException.class)
    public void startsWithServerError() throws Exception {
        new RtContainer(
            new AssertRequest(
                new Response(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, ""
                )
            ),
            URI.create("http://localhost:80/1.30/containers/123")
        ).start();
    }

    /**
     * RtContainer throws ISE if it receives "Not Found" on start.
     * @throws Exception If something goes wrong.
     */
    @Test(expected = UnexpectedResponseException.class)
    public void startsWithNotFound() throws Exception {
        new RtContainer(
            new AssertRequest(
                new Response(
                    HttpStatus.SC_NOT_FOUND, ""
                )
            ),
            URI.create("http://localhost:80/1.30/containers/123")
        ).start();
    }

    /**
     * RtContainer throws ISE if it receives "Not Modified" on start.
     * @throws Exception If something goes wrong.
     */
    @Test(expected = UnexpectedResponseException.class)
    public void startsWithNotModified() throws Exception {
        new RtContainer(
            new AssertRequest(
                new Response(
                    HttpStatus.SC_NOT_MODIFIED, ""
                )
            ),
            URI.create("http://localhost:80/1.30/containers/123")
        ).start();
    }
}
