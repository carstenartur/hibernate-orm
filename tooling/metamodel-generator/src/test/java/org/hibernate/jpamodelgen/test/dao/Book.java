package org.hibernate.jpamodelgen.test.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;

@Entity
public class Book {
    @Id String isbn;
    @NaturalId String title;
    @NaturalId String author;
    String text;
    int pages;
}
