/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.spi;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.context.TenantIdentifierMismatchException;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Base support for {@link CurrentSessionContext} implementors.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCurrentSessionContext implements CurrentSessionContext {
	private final SessionFactoryImplementor factory;

	protected AbstractCurrentSessionContext(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	/**
	 * Access to the SessionFactory
	 *
	 * @return The SessionFactory being serviced by this context
	 */
	public SessionFactoryImplementor factory() {
		return factory;
	}

	protected SessionBuilder baseSessionBuilder() {
		final SessionBuilderImplementor builder = factory.withOptions();
		final CurrentTenantIdentifierResolver<Object> resolver = factory.getCurrentTenantIdentifierResolver();
		if ( resolver != null ) {
			builder.tenantIdentifier( resolver.resolveCurrentTenantIdentifier() );
		}
		return builder;
	}

	protected void validateExistingSession(Session existingSession) {
		final CurrentTenantIdentifierResolver<Object> resolver = factory.getCurrentTenantIdentifierResolver();
		if ( resolver != null && resolver.validateExistingCurrentSessions() ) {
			final Object currentValue = resolver.resolveCurrentTenantIdentifier();
			final JavaType<Object> tenantIdentifierJavaType = factory.getTenantIdentifierJavaType();
			if ( !tenantIdentifierJavaType.areEqual( currentValue, existingSession.getTenantIdentifierValue() ) ) {
				throw new TenantIdentifierMismatchException(
						String.format(
								"Reported current tenant identifier [%s] did not match tenant identifier from " +
										"existing session [%s]",
								tenantIdentifierJavaType.toString( currentValue ),
								tenantIdentifierJavaType.toString( existingSession.getTenantIdentifierValue() )
						)
				);
			}
		}
	}
}
