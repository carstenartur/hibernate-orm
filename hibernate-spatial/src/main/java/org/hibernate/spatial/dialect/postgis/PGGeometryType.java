/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.WkbEncoder;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecoder;
import org.geolatte.geom.jts.JTS;
import org.postgresql.util.PGobject;

/**
 * Type Descriptor for the Postgis Geometry type
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class PGGeometryType implements JdbcType {


	private final Wkb.Dialect wkbDialect;

	// Type descriptor instance using EWKB v1 (postgis versions < 2.2.2)
	public static final PGGeometryType INSTANCE_WKB_1 = new PGGeometryType( Wkb.Dialect.POSTGIS_EWKB_1 );
	// Type descriptor instance using EWKB v2 (postgis versions >= 2.2.2, see: https://trac.osgeo.org/postgis/ticket/3181)
	public static final PGGeometryType INSTANCE_WKB_2 = new PGGeometryType( Wkb.Dialect.POSTGIS_EWKB_2 );

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		if ( javaTypeDescriptor instanceof GeolatteGeometryJavaTypeDescriptor ) {
			return (appender, value, dialect, wrapperOptions) -> {
				appender.appendSql( "ST_GeomFromEWKT('" );
				appender.appendSql( value.toString() );
				appender.appendSql( "')" );
			};
		}
		return (appender, value, dialect, wrapperOptions) -> {
			appender.appendSql( "ST_GeomFromEWKT('" );
			appender.appendSql( jts2Gl( value ).toString() );
			appender.appendSql( "')" );
		};
	}

	private <T> Geometry<?> jts2Gl(T value) {
		return JTS.from( (org.locationtech.jts.geom.Geometry) value );
	}

	private PGGeometryType(Wkb.Dialect dialect) {
		wkbDialect = dialect;
	}

	public Geometry<?> toGeometry(Object object) {
		if ( object == null ) {
			return null;
		}
		ByteBuffer buffer = null;
		if ( object instanceof PGobject ) {
			String pgValue = ( (PGobject) object ).getValue();

			if ( pgValue.startsWith( "00" ) || pgValue.startsWith( "01" ) ) {
				//we have a WKB because this pgValue starts with the bit-order byte
				buffer = ByteBuffer.from( pgValue );
				final WkbDecoder decoder = Wkb.newDecoder( wkbDialect );
				return decoder.decode( buffer );
			}
			else {
				return parseWkt( pgValue );
			}

		}
		throw new IllegalStateException( "Received object of type " + object.getClass().getCanonicalName() );
	}

	private static Geometry<?> parseWkt(String pgValue) {
		final WktDecoder decoder = Wkt.newDecoder( Wkt.Dialect.POSTGIS_EWKT_1 );
		return decoder.decode( pgValue );
	}


	@Override
	public int getJdbcTypeCode() {
		return Types.OTHER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return 5432;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final PGobject obj = toPGobject( value, options );
				st.setObject( index, obj );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final PGobject obj = toPGobject( value, options );
				st.setObject( name, obj );
			}

			private PGobject toPGobject(X value, WrapperOptions options) throws SQLException {
				final WkbEncoder encoder = Wkb.newEncoder( Wkb.Dialect.POSTGIS_EWKB_1 );
				final Geometry<?> geometry = getJavaTypeDescriptor().unwrap( value, Geometry.class, options );
				final String hexString = encoder.encode( geometry, ByteOrder.NDR ).toString();
				final PGobject obj = new PGobject();
				obj.setType( "geometry" );
				obj.setValue( hexString );
				return obj;
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {


			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( toGeometry( rs.getObject( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( toGeometry( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaTypeDescriptor().wrap( toGeometry( statement.getObject( name ) ), options );
			}
		};
	}
}