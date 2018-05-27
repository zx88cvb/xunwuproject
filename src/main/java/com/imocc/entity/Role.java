package com.imocc.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * Created by Administrator on 2018/1/5.
 */
@Entity
@Table(name="role")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    private String name;
}
