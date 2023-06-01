package com.example.moyiza_be.event.entity;

import com.example.moyiza_be.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
public class EventAttendant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "eventId")
    private Long eventId;
    @Column
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "userId")
    private Long userId;
    private String userName;

//    @Column
//    private boolean entrance;

    public EventAttendant(Long EventId, Long userId, String userName) {
        this.eventId = EventId;
        this.userId = userId;
        this.userName = userName;
    }

//    public void cancelAttendant(Event event) {
//        this.event = event;
//        this.entrance = false;
//    }
}
