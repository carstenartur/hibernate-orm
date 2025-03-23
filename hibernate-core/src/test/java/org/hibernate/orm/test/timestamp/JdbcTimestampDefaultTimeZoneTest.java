/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timestamp;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JdbcTimestampDefaultTimeZoneTest
		extends BaseSessionFactoryFunctionalTest {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(
	);

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		connectionProvider.setConnectionProvider( (ConnectionProvider) builder.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER ) );
		builder.applySetting(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@AfterAll
	protected void releaseResources() {
		connectionProvider.stop();
	}

	@Test
	public void testTimeZone() throws Throwable {

		connectionProvider.clear();
		inTransaction( s -> {
			Person person = new Person();
			person.id = 1L;
			s.persist( person );

		} );

		assertEquals( 1, connectionProvider.getPreparedStatements().size() );
		PreparedStatement ps = connectionProvider.getPreparedStatements()
				.get( 0 );
		List<Object[]> setTimeCalls = connectionProvider.spyContext.getCalls(
				PreparedStatement.class.getMethod( "setTimestamp", int.class, Timestamp.class ),
				ps
		);
		assertEquals( 1, setTimeCalls.size() );

		inTransaction( s -> {
			Person person = s.find( Person.class, 1L );
			assertEquals( 0, person.createdOn.getTime() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private Timestamp createdOn = new Timestamp( 0 );
	}
}
