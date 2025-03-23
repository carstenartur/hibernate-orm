/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.hana;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.Point;

public class HANAPointType implements JdbcType {

	/**
	 * An instance of the descrtiptor
	 */
	public static final HANAPointType INSTANCE = new HANAPointType();
	private static final long serialVersionUID = -6978798264716544804L;

	@Override
	public int getJdbcTypeCode() {
		return Types.STRUCT;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.POINT;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<X>( javaType, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final Point<?> geometry = getJavaType().unwrap( value, Point.class, options );
				st.setObject( index, HANASpatialUtils.toEWKB( geometry ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Point<?> geometry = getJavaType().unwrap( value, Point.class, options );
				st.setObject( name, HANASpatialUtils.toEWKB( geometry ) );
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<X>( javaType, this ) {

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( HANASpatialUtils.toGeometry( rs.getObject( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( HANASpatialUtils.toGeometry( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap( HANASpatialUtils.toGeometry( statement.getObject( name ) ), options );
			}
		};
	}

}
