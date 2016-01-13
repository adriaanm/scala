package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class TypeAlias extends TypeMember
{
        public TypeAlias(Type tpe, TypeParameter[] typeParameters, String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                super(typeParameters, name, access, modifiers, annotations);
                this.tpe = tpe;
        }
        private final Type tpe;
        public final Type tpe()
        {
                return tpe;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof TypeAlias)) {
                         return false;
                } else {
                        TypeAlias o = (TypeAlias)obj;
                        return tpe().equals(o.tpe()) && Arrays.deepEquals(typeParameters(), o.typeParameters()) && name().equals(o.name()) && access().equals(o.access()) && modifiers().equals(o.modifiers()) && Arrays.deepEquals(annotations(), o.annotations());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (37 * (37 * (37 * (37 * (17) + tpe().hashCode()) + Arrays.deepHashCode(typeParameters())) + name().hashCode()) + access().hashCode()) + modifiers().hashCode()) + Arrays.deepHashCode(annotations());
        }
        public String toString()
        {
                 return "TypeAlias(" + "tpe: " + tpe() + ", " + "typeParameters: " + Arrays.toString(typeParameters()) + ", " + "name: " + name() + ", " + "access: " + access() + ", " + "modifiers: " + modifiers() + ", " + "annotations: " + Arrays.toString(annotations()) + ")";
        }
}
