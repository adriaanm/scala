package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Var extends FieldLike
{
        public Var(Type tpe, String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                super(tpe, name, access, modifiers, annotations);

        }

        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Var)) {
                         return false;
                } else {
                        Var o = (Var)obj;
                        return tpe().equals(o.tpe()) && name().equals(o.name()) && access().equals(o.access()) && modifiers().equals(o.modifiers()) && Arrays.deepEquals(annotations(), o.annotations());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (37 * (37 * (37 * (17) + tpe().hashCode()) + name().hashCode()) + access().hashCode()) + modifiers().hashCode()) + Arrays.deepHashCode(annotations());
        }
        public String toString()
        {
                 return "Var(" + "tpe: " + tpe() + ", " + "name: " + name() + ", " + "access: " + access() + ", " + "modifiers: " + modifiers() + ", " + "annotations: " + Arrays.toString(annotations()) + ")";
        }
}
