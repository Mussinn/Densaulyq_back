package com.example.MedSafe.interceptor;

import java.security.Principal;

public class UsernamePrincipal implements Principal {
    private final String name;

    public UsernamePrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsernamePrincipal that = (UsernamePrincipal) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
