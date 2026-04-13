import { sleep } from "k6";
import http from "k6/http";
import { type Options } from "k6/options";

export default function () {
  http.get("https://k6.io");
  sleep(1);
}

export const options: Options = {
  iterations: 10,
};
