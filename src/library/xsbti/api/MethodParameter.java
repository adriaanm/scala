package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class MethodParameter implements java.io.Serializable
{
        public MethodParameter(String name, Type tpe, boolean hasDefault, ParameterModifier modifier)
        {
                this.name = name;
                this.tpe = tpe;
                this.hasDefault = hasDefault;
                this.modifier = modifier;
        }
        private final String name;
        private final Type tpe;
        private final boolean hasDefault;
        private final ParameterModifier modifier;
        public final String name()
        {
                return name;
        }
        public final Type tpe()
        {
                return tpe;
        }
        public final boolean hasDefault()
        {
                return hasDefault;
        }
        public final ParameterModifier modifier()
        {
                return modifier;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof MethodParameter)) {
                         return false;
                } else {
                        MethodParameter o = (MethodParameter)obj;
                        return name().equals(o.name()) && tpe().equals(o.tpe()) && hasDefault() == o.hasDefault() && modifier().equals(o.modifier());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (37 * (37 * (17) + name().hashCode()) + tpe().hashCode()) + (hasDefault() ? 0 : 1)) + modifier().hashCode();
        }
        public String toString()
        {
                 return "MethodParameter(" + "name: " + name() + ", " + "tpe: " + tpe() + ", " + "hasDefault: " + hasDefault() + ", " + "modifier: " + modifier() + ")";
        }
}
