package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.Limit;
import dev.morphia.aggregation.experimental.Lookup;
import dev.morphia.aggregation.experimental.codecs.stages.AddFieldsCodec;
import dev.morphia.aggregation.experimental.codecs.stages.GroupCodec;
import dev.morphia.aggregation.experimental.codecs.stages.LimitCodec;
import dev.morphia.aggregation.experimental.codecs.stages.LookupCodec;
import dev.morphia.aggregation.experimental.codecs.stages.MatchCodec;
import dev.morphia.aggregation.experimental.codecs.stages.ProjectionCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SampleCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SortCodec;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.mapping.Mapper;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class AggregationCodecProvider implements CodecProvider {

    private Map<Class, Codec> codecs;
    private Codec expressionCodec;
    private Mapper mapper;

    public AggregationCodecProvider(final Mapper mapper) {
        this.mapper = mapper;
        expressionCodec = new ExpressionCodec(this.mapper);
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        Codec<T> codec = getCodecs().get(clazz);
        if (codec == null) {
            if (Expression.class.isAssignableFrom(clazz)) {
                codec = expressionCodec;
            }
        }
        return codec;
    }

    private Map<Class, Codec> getCodecs() {
        if (codecs == null) {
            codecs = new HashMap<>();

            // Stages
            codecs.put(Group.class, new GroupCodec(mapper));
            codecs.put(Sample.class, new SampleCodec(mapper));
            codecs.put(Projection.class, new ProjectionCodec(mapper));
            codecs.put(Sort.class, new SortCodec(mapper));
            codecs.put(Limit.class, new LimitCodec(mapper));
            codecs.put(Lookup.class, new LookupCodec(mapper));
            codecs.put(Match.class, new MatchCodec(mapper));
            codecs.put(AddFields.class, new AddFieldsCodec(mapper));
        }
        return codecs;
    }
}