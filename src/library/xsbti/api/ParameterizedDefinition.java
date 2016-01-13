package xsbti.api;

import java.util.Arrays;
import java.util.List;
public abstract class ParameterizedDefinition extends Definition
{
        public ParameterizedDefinition(TypeParameter[] typeParameters, String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                super(name, access, modifiers, annotations);
                this.typeParameters = typeParameters;
        }
        private final TypeParameter[] typeParameters;
        public final TypeParameter[] typeParameters()
        {
                return typeParameters;
        }
}
