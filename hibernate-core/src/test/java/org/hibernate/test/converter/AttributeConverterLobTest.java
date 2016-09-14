/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import static org.junit.Assert.assertEquals;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.Query;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * HHH-11098
 * Test to check the number of attributeconverter calls on a simple save and list
 *
 * @author Carsten Hammer
 */
public class AttributeConverterLobTest extends BaseCoreFunctionalTestCase {
	public AttributeConverterLobTest() {
		
	}
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityImpl.class };
	}
	
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testMappingAttributeWithLobAndAttributeConverter() {
		Session session = openSession();
		session.beginTransaction();
		EntityImpl object = new EntityImpl();
		object.status=new MyMap<>();
		object.status.put( "asdf", Integer.valueOf( 6 ) );
		object.status.put( "key", "table" );
		object.id=1;
		session.save( object );
		session.getTransaction().commit();
		session.close();
		/**
		 * Thanks to Steve Ebersole now only 1 access using @Immutable annotation
		 */
		assertEquals(1,ConverterImpl.todatabasecounter);
		assertEquals(0,ConverterImpl.fromdatabasecounter);
		
		session = openSession();
		session.beginTransaction();
		Query<EntityImpl> createQuery = session.createQuery( "select e from EntityImpl e", EntityImpl.class );
		List<EntityImpl> resultList = createQuery.getResultList();
		assertEquals(1,resultList.size());
		session.getTransaction().commit();
		session.close();
		
		assertEquals(1,ConverterImpl.todatabasecounter);
		assertEquals(1,ConverterImpl.fromdatabasecounter);
		assertEquals("table",resultList.get(0 ).status.get( "key" ));
		assertEquals(1,ConverterImpl.fromdatabasecounter);
	}

	@Converter
	public static class ConverterImpl implements AttributeConverter<MyMap<String, Object>, byte[]> {
		public static int todatabasecounter=0;
		public static int fromdatabasecounter=0;
		@Override
		public byte[] convertToDatabaseColumn(MyMap<String, Object> map) {
			todatabasecounter++;
			ByteArrayOutputStream out=new ByteArrayOutputStream();
			try(XMLEncoder encoder=new XMLEncoder(out)){
				encoder.writeObject( map );
				encoder.flush();
			}
			return out.toByteArray();
		}

		@Override
		public MyMap<String, Object> convertToEntityAttribute(byte[] dbData) {
			fromdatabasecounter++;
			try(ByteArrayInputStream in=new ByteArrayInputStream(dbData);
					XMLDecoder decoder=new XMLDecoder(in)){
				return (MyMap<String, Object>) decoder.readObject();
			}
			catch (IOException e) {
				return null;
			}
		}
	}

	@Entity(name = "EntityImpl")
	@Table( name = "EntityImpl" )
	public static class EntityImpl implements Serializable {
		@Id
		private Integer id;

		@Lob
		@Convert(converter = ConverterImpl.class)
		private MyMap<String, Object> status;
	}
	
	@Immutable
	public static class MyMap<String, Object> extends HashMap<String, Object>{
	}
}
