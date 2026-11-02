package com.proyecto.macrofit.usuarios.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Usuario {
    private Integer id_usuario;
    private Integer id_objetivo;
    private Integer id_nv_act;
    private String correo;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasena;
    private String rol;
    private String nom_usuario;
    private Integer edad;
    private float peso;
    private Integer altura;
    private String sexo;
    private float tmb_objetivo;
    private float cal_diaria;

    public Integer getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(Integer id_usuario) {
        this.id_usuario = id_usuario;
    }

    public Integer getId_objetivo() {
        return id_objetivo;
    }

    public void setId_objetivo(Integer id_objetivo) {
        this.id_objetivo = id_objetivo;
    }

    public Integer getId_nv_act() {
        return id_nv_act;
    }

    public void setId_nv_act(Integer id_nv_act) {
        this.id_nv_act = id_nv_act;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNom_usuario() {
        return nom_usuario;
    }

    public void setNom_usuario(String nom_usuario) {
        this.nom_usuario = nom_usuario;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public float getPeso() {
        return peso;
    }

    public void setPeso(float peso) {
        this.peso = peso;
    }

    public Integer getAltura() {
        return altura;
    }

    public void setAltura(Integer altura) {
        this.altura = altura;
    }

    public float getTmb_objetivo() {
        return tmb_objetivo;
    }

    public void setTmb_objetivo(float tmb_objetivo) {
        this.tmb_objetivo = tmb_objetivo;
    }

    public float getCal_diaria() {
        return cal_diaria;
    }

    public void setCal_diaria(float cal_diaria) {
        this.cal_diaria = cal_diaria;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

}