/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.support;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.domain.Profile;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;

/**
 * Unit tests for {@link DefaultSelfLinkProvider}.
 * 
 * @author Oliver Gierke
 * @soundtrack Trio Rotation - Triopane
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSelfLinkProviderUnitTests {

	SelfLinkProvider provider;

	@Mock EntityLinks entityLinks;
	PersistentEntities entities;
	List<EntityLookup<?>> lookups;

	public @Rule ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() {

		when(entityLinks.linkToSingleResource((Class<?>) any(), any())).then(new Answer<Link>() {

			@Override
			public Link answer(InvocationOnMock invocation) throws Throwable {

				Class<?> type = invocation.getArgumentAt(0, Class.class);
				Serializable id = invocation.getArgumentAt(1, Serializable.class);

				return new Link("/".concat(type.getName()).concat("/").concat(id.toString()));
			}
		});

		KeyValueMappingContext context = new KeyValueMappingContext();
		context.getPersistentEntity(Profile.class);
		context.afterPropertiesSet();

		this.entities = new PersistentEntities(Arrays.asList(context));
		this.lookups = Collections.emptyList();
		this.provider = new DefaultSelfLinkProvider(entities, entityLinks, lookups);
	}

	/**
	 * @see DATAREST-724
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullEntities() {
		new DefaultSelfLinkProvider(null, entityLinks, lookups);
	}

	/**
	 * @see DATAREST-724
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullEntityLinks() {
		new DefaultSelfLinkProvider(entities, null, lookups);
	}

	/**
	 * @see DATAREST-724
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullEntityLookups() {
		new DefaultSelfLinkProvider(entities, entityLinks, null);
	}

	/**
	 * @see DATAREST-724
	 */
	@Test
	public void usesEntityIdIfNoLookupDefined() {

		Profile profile = new Profile("Name", "Type");
		Link link = provider.createSelfLinkFor(profile);

		assertThat(link.getHref(), Matchers.endsWith(profile.getId().toString()));
	}

	/**
	 * @see DATAREST-724
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void usesEntityLookupIfDefined() {

		EntityLookup<Object> lookup = mock(EntityLookup.class);
		when(lookup.supports(Profile.class)).thenReturn(true);
		when(lookup.getResourceIdentifier(any(Profile.class))).thenReturn("foo");

		this.provider = new DefaultSelfLinkProvider(entities, entityLinks, Arrays.asList(lookup));

		Link link = provider.createSelfLinkFor(new Profile("Name", "Type"));

		assertThat(link.getHref(), Matchers.endsWith("foo"));
	}

	/**
	 * @see DATAREST-724
	 */
	@Test
	public void rejectsLinkCreationForUnknownEntity() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(Object.class.getName());
		exception.expectMessage("No persistent entity found!");

		provider.createSelfLinkFor(new Object());
	}
}
