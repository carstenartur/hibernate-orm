/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.Type;

import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * The legacy id generator named {@code uuid} / {@code uuid.hex}.
 * <p>
 * A {@link UUIDGenerator} that returns a string of length 32,
 * This string will consist of only hex digits. Optionally,
 * the string may be generated with separators between each
 * component of the UUID.
 * <p>
 * Mapping parameter supported: {@value #SEPARATOR}.
 *
 * @author Gavin King
 *
 * @deprecated This remains around as an implementation detail of {@code hbm.xml} mappings.
 */
@Deprecated(since = "6")
public class UUIDHexGenerator extends AbstractUUIDGenerator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( UUIDHexGenerator.class );

	/**
	 * The configuration parameter specifying the separator to use.
	 */
	public static final String SEPARATOR = "separator";

	private static boolean WARNED;

	private String sep = "";

	public UUIDHexGenerator() {
		if ( !WARNED ) {
			WARNED = true;
			LOG.usingUuidHexGenerator( this.getClass().getName(), UUIDGenerator.class.getName() );
		}
	}


	@Override
	public void configure(GeneratorCreationContext creationContext, Properties parameters) throws MappingException {
		sep = getString( SEPARATOR, parameters, "" );
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object obj) {
		return format( getIP() ) + sep
				+ format( getJVM() ) + sep
				+ format( getHiTime() ) + sep
				+ format( getLoTime() ) + sep
				+ format( getCount() );
	}

	protected String format(int intValue) {
		String formatted = Integer.toHexString( intValue );
		StringBuilder buf = new StringBuilder( "00000000" );
		buf.replace( 8 - formatted.length(), 8, formatted );
		return buf.toString();
	}

	protected String format(short shortValue) {
		String formatted = Integer.toHexString( shortValue );
		StringBuilder buf = new StringBuilder( "0000" );
		buf.replace( 4 - formatted.length(), 4, formatted );
		return buf.toString();
	}
}
