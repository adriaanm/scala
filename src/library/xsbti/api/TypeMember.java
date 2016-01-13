package xsbti.api;

import java.util.Arrays;
import java.util.List;
public abstract class TypeMember extends ParameterizedDefinition
{
        public TypeMember(TypeParameter[] typeParameters, String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                super(typeParameters, name, access, modifiers, annotations);

        }

}
