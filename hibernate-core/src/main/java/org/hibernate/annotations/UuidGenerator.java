/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that an entity identifier is generated as an
 * <a href=https://datatracker.ietf.org/doc/html/rfc4122>IETF RFC 4122 UUID</a>.
 * <p>
 * The type of the identifier attribute may be {@link UUID} or {@link String}.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@IdGeneratorType(org.hibernate.id.uuid.UuidGenerator.class)
@ValueGenerationType(generatedBy = org.hibernate.id.uuid.UuidGenerator.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface UuidGenerator {

	/**
	 * Represents a kind of UUID, that is, what RFC 4122 calls a "version".
	 */
	enum Style {
		/**
		 * Defaults to {@link #RANDOM}.
		 */
		AUTO,
		/**
		 * Use {@link UUID#randomUUID()} to generate UUIDs, producing a value
		 * compatible with RFC 4122 version 4.
		 */
		RANDOM,
		/**
		 * Use a time-based generation strategy consistent with RFC 4122
		 * version 1, but with IP address instead of MAC address.
		 *
		 * @implNote Can be a bottleneck, since synchronization is used when
		 *           incrementing an internal counter as part of the algorithm.
		 */
		TIME
	}

	/**
	 * Specifies which {@linkplain Style style} of UUID generation should be used.
	 */
	Style style() default Style.AUTO;
}
