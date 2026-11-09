module.exports = {
  '^/(auth|users|products|media|ecom-media)(/|$)': {
    target: `http://localhost:${process.env.GATEWAY_PORT || '6200'}`,
    changeOrigin: true,
  },
};
