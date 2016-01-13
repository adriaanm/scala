package xsbti.api;

import java.util.Arrays;
import java.util.List;
public abstract class Qualified extends Access
{
        public Qualified(Qualifier qualifier)
        {
                this.qualifier = qualifier;
        }
        private final Qualifier qualifier;
        public final Qualifier qualifier()
        {
                return qualifier;
        }
}
