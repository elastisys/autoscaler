package com.elastisys.autoscaler.core.prediction.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Objects;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * A capacity limit rule that provides scheduled min-max boundaries for the
 * produced compute unit need predictions.
 * <p/>
 * A capacity limit rule can, for example, be <i>scheduled</i> to handle planned
 * peaks/lows in demand or rules can be <i>fixed</i> (always active) to place a
 * budget ceiling on the resource pool.
 * <p/>
 * The relative importance of different capacity limit rules is determined by
 * their rank. A high rank means high importance. The highest ranked capacity
 * limit that is active at a given time dictates the resource pool boundaries of
 * the elasticity engine.
 * <p/>
 * A capacity limit rule uses a cron schedule to dictate when the limit is to be
 * in effect. The schedule is required to follow the <a href=
 * "http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">Quartz
 * cron expression syntax</a>. The following are examples of activation
 * schedules:
 *
 * <code>
 *   <table border="1">
 *     <tr>
 *       <th>Cron expression</th>
 *       <th>Activation semantics</th>
 *     </tr>
 *     <tr>
 *       <td>* * * * * ? *</td>
 *       <td>Always active.</td>
 *     </tr>
 *     <tr>
 *       <td>* * 0-23 L * ? *</td>
 *       <td>Active the (entire) last day of the month - [00:00:00, 23:59:59] </td>
 *     </tr>
 *     <tr>
 *       <td>* * 10-21 L * ? *</td>
 *       <td>Active the last day of the month between [10:00:00,21:59:59].</td>
 *     </tr>
 *     <tr>
 *       <td>* * 10-21 ? * FRI *</td>
 *       <td>Active every Friday between [10:00:00,21:59:59].</td>
 *     </tr>
 *     <tr>
 *       <td>* * 10-21 LW * ? *</td>
 *       <td>Active on the last weekday of every month between [10:00:00,21:59:59].</td>
 *     </tr>
 *   </table>
 * </code>
 */
public class CapacityLimitConfig implements Comparable<CapacityLimitConfig> {

    /** The unique identifier of this capacity limit. */
    private final String id;
    /**
     * The rank (precedence) of this capacity limit. If more than one capacity
     * limit is active at the same time, the rank is used to select the capacity
     * limit in effect. Highest rank wins.
     */
    private final Long rank;
    /**
     * The cron expression that dictates when when this capacity limit is
     * active. The expressions will always be evaluated against UTC time.
     */
    private final String schedule;

    /**
     * The minimum number of compute units admitted by this capacity limit rule.
     */
    private final Integer min;
    /**
     * The maximum number of compute units admitted by this capacity limit rule.
     */
    private final Integer max;

    public CapacityLimitConfig(String id, long rank, String schedule, int min, int max) {
        this.id = id;
        this.rank = rank;
        this.schedule = schedule;
        this.min = min;
        this.max = max;
    }

    public String getId() {
        return this.id;
    }

    public long getRank() {
        return this.rank;
    }

    public String getSchedule() {
        return this.schedule;
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    /**
     * Returns <code>true</code> if this {@link CapacityLimitType} is in effect
     * at the given time (converted to UTC). A capacity limit that is not in
     * effect can be disregarded by the elasticity engine.
     *
     * @param time
     *            The time of interest. Note that the time will be converted to
     *            UTC before being evaluated against the cron expression.
     * @return <code>true</code> if this {@link CapacityLimitType} is currently
     *         in effect, <code>false</code> otherwise.
     */
    public boolean inEffectAt(DateTime time) {
        ZonedDateTime timestamp = ZonedDateTime.parse(time.withZone(DateTimeZone.UTC).toString());
        ExecutionTime cronSchedule = ExecutionTime.forCron(parseCronExpression(this.schedule));
        return cronSchedule.isMatch(timestamp);
    }

    /**
     * Parses a schedule expressed as a cron expression. The schedule is
     * required to follow the <a href=
     * "http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">Quartz
     * cron expression syntax</a> using fields similar to:
     *
     * <pre>
     * SECONDS MINUTES HOURS DAYOFMONTH MONTH    DAYOFWEEK [YEAR]
     * *       *       *     *          *        ?         *
     * 0/5     3-39    14-17 ?          JAN,MAR  MON-FRI   2002-2010
     * </pre>
     *
     * @param schedule
     *            a schedule as a cron expression.
     * @return The corresponding {@link CronExpression}.
     */
    public static Cron parseCronExpression(String schedule) throws IllegalArgumentException {
        try {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
            CronParser parser = new CronParser(definition);
            Cron cronExpression = parser.parse(schedule);
            return cronExpression;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(
                    "capacityLimit: schedule is not a valid cron expression: %s: %s", schedule, e.getMessage()), e);
        }
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.id != null, "capacityLimit: missing id");
        checkArgument(this.rank != null, "capacityLimit: missing rank");
        checkArgument(this.schedule != null, "capacityLimit: missing schedule");
        parseCronExpression(this.schedule);
        checkArgument(this.min != null, "capacityLimit: missing min");
        checkArgument(this.min >= 0, "capacityLimit: min must be non-negative");
        checkArgument(this.max != null, "capacityLimit: missing max");
        checkArgument(this.min <= this.max, "capacityLimit: illegal limit: min (%s) > max (%s)", this.min, this.max);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CapacityLimitConfig) {
            CapacityLimitConfig that = (CapacityLimitConfig) obj;
            return Objects.equals(this.id, that.id) //
                    && Objects.equals(this.rank, that.rank) //
                    && Objects.equals(this.schedule, that.schedule) //
                    && Objects.equals(this.min, that.min) //
                    && Objects.equals(this.max, that.max);

        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.rank, this.schedule, this.min, this.max);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }

    @Override
    public int compareTo(CapacityLimitConfig other) {
        return Comparator.comparing(CapacityLimitConfig::getRank).thenComparing(CapacityLimitConfig::getId)
                .compare(this, other);
    }
}