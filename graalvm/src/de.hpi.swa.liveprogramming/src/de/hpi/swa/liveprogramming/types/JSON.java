package de.hpi.swa.liveprogramming.types;

import com.oracle.truffle.tools.utils.json.JSONArray;
import com.oracle.truffle.tools.utils.json.JSONException;
import com.oracle.truffle.tools.utils.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JSON {
	public static <T> List<T> map(JSONArray json, Function<JSONObject, T> itemParser) {
		try {
			return IntStream.range(0, json.length())
					.boxed()
					.map(i -> itemParser.apply(json.getJSONObject(i)))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		} catch (JSONException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public static <T> T orDefault(JSONObject json, String key, T def, BiFunction<JSONObject, String, T> getter) {
		try {
			return getter.apply(json, key);
		} catch (JSONException e) {
			return def;
		}
	}
}
