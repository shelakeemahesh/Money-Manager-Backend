package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}
