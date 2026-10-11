package com.vijaya.theatre.movie;

import com.vijaya.theatre.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-01–FR-03, UC-04. The movie catalogue shows are scheduled against. */
@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
public class Movie extends AuditableEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String genre;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(columnDefinition = "text")
    private String synopsis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieStatus status = MovieStatus.UPCOMING;
}
