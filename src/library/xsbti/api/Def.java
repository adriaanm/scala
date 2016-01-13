package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Def extends ParameterizedDefinition
{
        public Def(ParameterList[] valueParameters, Type returnType, TypeParameter[] typeParameters, String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                super(typeParameters, name, access, modifiers, annotations);
                this.valueParameters = valueParameters;
                this.returnType = returnType;
        }
        private final ParameterList[] valueParameters;
        private final Type returnType;
        public final ParameterList[] valueParameters()
        {
                return valueParameters;
        }
        public final Type returnType()
        {
                return returnType;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Def)) {
                         return false;
                } else {
                        Def o = (Def)obj;
                        return Arrays.deepEquals(valueParameters(), o.valueParameters()) && returnType().equals(o.returnType()) && Arrays.deepEquals(typeParameters(), o.typeParameters()) && name().equals(o.name()) && access().equals(o.access()) && modifiers().equals(o.modifiers()) && Arrays.deepEquals(annotations(), o.annotations());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (37 * (37 * (37 * (37 * (37 * (17) + Arrays.deepHashCode(valueParameters())) + returnType().hashCode()) + Arrays.deepHashCode(typeParameters())) + name().hashCode()) + access().hashCode()) + modifiers().hashCode()) + Arrays.deepHashCode(annotations());
        }
        public String toString()
        {
                 return "Def(" + "valueParameters: " + Arrays.toString(valueParameters()) + ", " + "returnType: " + returnType() + ", " + "typeParameters: " + Arrays.toString(typeParameters()) + ", " + "name: " + name() + ", " + "access: " + access() + ", " + "modifiers: " + modifiers() + ", " + "annotations: " + Arrays.toString(annotations()) + ")";
        }
}
