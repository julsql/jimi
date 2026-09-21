package com.tsp.jimi_api.entities;

import com.tsp.jimi_api.enums.Type;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Date;
import java.sql.Time;

/**
 * The table Agenda.
 */
@Entity
public class Agenda {

    /**
     * The id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * The Date.
     */
    private Date date;
    /**
     * The Beginning Time.
     */
    @Column(name = "begin_time")
    private Time begin;
    /**
     * The End Time.
     */
    @Column(name = "end_time")
    private Time end;
    /**
     * The type.
     */
    @Enumerated(EnumType.STRING)
    private Type type;
    /**
     * The title.
     */
    private String title;
    /**
     * The user id.
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * Instantiates a new agenda.
     */
    public Agenda() {
    }

    /**
     * Instantiates a new Agenda.
     *
     * @param date   the date
     * @param begin  the beginning
     * @param end    the end
     * @param type   the type
     * @param title  the title
     * @param userId the user id
     */
    public Agenda(final Date date, final Time begin, final Time end,
                  final Type type, final String title, final String userId) {
        this.date = date;
        this.begin = begin;
        this.end = end;
        this.type = type;
        this.title = title;
        this.userId = userId;
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets date.
     *
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Sets date.
     *
     * @param date the date
     */
    public void setDate(final Date date) {
        this.date = date;
    }

    /**
     * Gets begin.
     *
     * @return the begin
     */
    public Time getBegin() {
        return begin;
    }

    /**
     * Sets begin.
     *
     * @param begin the begin
     */
    public void setBegin(final Time begin) {
        this.begin = begin;
    }

    /**
     * Gets end.
     *
     * @return the end
     */
    public Time getEnd() {
        return end;
    }

    /**
     * Sets end.
     *
     * @param end the end
     */
    public void setEnd(final Time end) {
        this.end = end;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets type.
     *
     * @param type the type
     */
    public void setType(final Type type) {
        this.type = type;
    }

    /**
     * Gets title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets title.
     *
     * @param title the title
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Gets mac.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets user id.
     *
     * @param userId the user id
     */
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * To string string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "{"
                + "\"id\": " + id
                + ", \"date\": " + date
                + ", \"begin\": " + begin
                + ", \"end\": " + end
                + ", \"type\": " + type
                + ", \"title\": " + title
                + "}";
    }
}
