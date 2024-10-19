# frozen_string_literal: true

RSpec.describe Cedarling do
  it "has a version number" do
    expect(Cedarling::VERSION).not_to be nil
  end

  it "calls into the rust extension" do
    expect(Cedarling::hello('Finnegan')).to eq('Hello from Rust, Finnegan!')
  end
end
