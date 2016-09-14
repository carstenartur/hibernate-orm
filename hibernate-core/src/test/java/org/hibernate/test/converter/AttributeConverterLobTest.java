/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.Query;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test to check the number of attributeconverter calls on a simple save and list
 *
 * @author Carsten Hammer
 */
public class AttributeConverterLobTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityImpl.class };
	}
	
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
//		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testMappingAttributeWithLobAndAttributeConverter() {
		Session session = openSession();
		session.beginTransaction();
		EntityImpl object = new EntityImpl();
		object.status=new HashMap<>();
		object.status.put( "asdf", Integer.valueOf( 6 ) );
		object.status.put( "key", "table" );
		object.id=1;
		session.save( object );
		session.getTransaction().commit();
		session.close();
		/**
		 * What? Why the hell 2 and not 1?
		 */
		assertEquals(2,ConverterImpl.todatabasecounter);
		/**
		 * Why a from database conversion at all?
		 */
		assertEquals(1,ConverterImpl.fromdatabasecounter);
		
		session = openSession();
		session.beginTransaction();
		Query<EntityImpl> createQuery = session.createQuery( "select e from EntityImpl e", EntityImpl.class );
		List<EntityImpl> resultList = createQuery.getResultList();
		assertEquals(1,resultList.size());
		session.getTransaction().commit();
		session.close();
		/**
		 * Why again a to database conversion? These conversions are very expensive and should only be done if really needed..
		 */
		assertEquals(3,ConverterImpl.todatabasecounter);
		assertEquals(3,ConverterImpl.fromdatabasecounter);
		assertEquals("table",resultList.get(0 ).status.get( "key" ));
		assertEquals(3,ConverterImpl.fromdatabasecounter);
	}

	@Converter
	public static class ConverterImpl implements AttributeConverter<Map, byte[]> {
		public static int todatabasecounter=0;
		public static int fromdatabasecounter=0;
		@Override
		public byte[] convertToDatabaseColumn(Map map) {
			todatabasecounter++;
			ByteArrayOutputStream out=new ByteArrayOutputStream();
			try(XMLEncoder encoder=new XMLEncoder(out)){
				encoder.writeObject( map );
			}
			return out.toByteArray();
		}

		@Override
		public Map convertToEntityAttribute(byte[] dbData) {
			fromdatabasecounter++;
			try(ByteArrayInputStream in=new ByteArrayInputStream(dbData)){
				XMLDecoder decoder=new XMLDecoder(in);
				return (Map) decoder.readObject();
			}
			catch (IOException e) {
				return null;
			}
		}
	}

	@Entity(name = "EntityImpl")
	@Table( name = "EntityImpl" )
	public static class EntityImpl {
		@Id
		private Integer id;

		@Lob
		@Convert(converter = ConverterImpl.class)
		private Map status;
	}
}
